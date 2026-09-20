# kmp-spec-driven-skills verification matrix

## Source inspected

- Repository: `SOSMex/kmp-spec-driven-skills`
- Inspected commit: `66d05bb416ba2276b7f62acac948e34050aefc7e`
- License: Apache-2.0
- Upstream validator before local correction: `Validation passed: 2 skills, 8 eval cases, 20 Markdown files.`

## Matrix

| Skill | Installation | Discovery | Invocation in this SDK | Artifacts produced | Errors or ambiguities | Local correction / recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| `skill-installer` | Preinstalled system skill; read completely before use | Present in original catalog | Official `install-skill-from-github.py --repo SOSMex/kmp-spec-driven-skills --path skills/kmp-spec-driven-design skills/kmp-proof-of-parity` | Global installs under `~/.codex/skills/` | Correctly warns that new skills are available on a subsequent turn | No correction needed |
| `kmp-spec-driven-design` | Installed by official script | Not dynamically added to the installing turn; discovered by name in a newly opened Codex task | Classified external-provider/lifecycle/identity/compatibility work as RFC-level risk; assigned common versus Android/iOS ownership; defined degraded states and target-aware evidence | `spec.md`, RFC 0001, `plan.md`, `tasks.md`, acceptance criteria and verification plan | Installed `SKILL.md` linked `../../templates/`, which did not exist after installing only the skill directory; `RTK.md` referenced by workspace instructions was absent | Changed link to `templates/`; added self-contained spec/RFC/ADR/plan/task templates to local clone and installed copy. Recommend upstream make each advertised self-contained skill include its templates and add an eval for portable installation |
| `kmp-proof-of-parity` | Installed by official script | Discovered by name in the new Codex task | Separated automated tests, target compilation, simulator execution, Maven-local publication, physical-device evidence and public release | `verification-report.md`, this matrix and `handoff.md` | Same broken `../../templates/verification-report.md` path after official subdirectory install | Changed link to `templates/verification-report.md`; added verification/handoff templates locally and to installed copy. Recommend validator test the actual installed subdirectory shape, not only links valid from repository root |

## Post-correction verification

- Local clone validator: `Validation passed: 2 skills, 8 eval cases, 27 Markdown files.`
- `git diff --check` on the local skill clone: passed.
- A subsequent read-only Codex turn confirmed both skills appeared by name in the catalog.
- A second subsequent turn confirmed all corrected template links resolved inside each installed skill directory.
- The focused correction is committed only in the local clone as `0cad6a512c144233b13073b728ca78eaaaf12c3e`; no remote, release, PR or push was created.

## Invocation influence

The design skill forced the project to create behavior and evidence artifacts before code and led to the explicit `Invoked` identity result instead of a misleading success/acceptance state. The parity skill prevented compilation and simulator tests from being described as physical push parity or release evidence.
