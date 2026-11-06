# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| --------------------------- | --------------------- | ----------------------------------------- |
| `needs-triage`               | `needs-triage`        | Maintainer needs to evaluate this issue   |
| `needs-info`                 | `needs-info`          | Waiting on reporter for more information  |
| `ready-for-agent`            | `ready-for-agent`     | Fully specified, ready for an AFK agent   |
| `ready-for-human`            | `ready-for-human`     | Requires human implementation             |
| `wontfix`                    | `wontfix`             | Will not be actioned                      |

Besides the triage roles this repo uses one extra label:

| Label  | Meaning                                                                                                   |
| ------ | --------------------------------------------------------------------------------------------------------- |
| `epic` | Umbrella issue (spec) split into sub-tickets. Not implementable itself; pick a `ready-for-agent` sub-ticket. |

An issue is an `epic` from the moment `to-tickets` splits it: it loses `ready-for-agent` and gets `epic`.

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

Edit the "Label in our tracker" column to match whatever vocabulary you actually use.
