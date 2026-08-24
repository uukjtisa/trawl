# design/ — the UI contract

Trawl's UI was designed as an interactive mockup **before** any Compose code, and the mockup is
the spec. Where the mockup and the app disagree, **the mockup wins** — or the disagreement gets
raised explicitly and recorded. It is never resolved silently in the code.

| File | What it is |
|---|---|
| `v0.1.0-baseline-mockup-ui.html` | **The baseline.** A self-contained, interactive mockup: 6 screens, 7 themes, 3 glass levels, 2 motion styles, the floating download bubble, the intro sequence. Open it in a browser; the in-phone Settings actually drive it. |
| `v0.1.0-implementation-spec.html` | **The contract.** Request log in order, design tokens, component index, motion index, state model, behaviour rules, acceptance checklist — every entry citing the exact line in the mockup that defines it. |
| `genspec.py` | Regenerates the spec from the mockup. |

## Rules

1. **Copy values, don't match them by eye.** Colours, durations, easings and sizes are all
   tabulated in the spec as literals.
2. **Re-run `genspec.py` after editing the mockup.** Citations are extracted, not typed — a
   stale citation is worse than none.
   ```
   cd design && python genspec.py
   ```
3. **Deviations are a conversation.** If Compose makes an approved behaviour awkward, build it
   anyway or raise it. Never substitute silently; never report it done when it was simplified.
4. **Decision rationale lives in `../DECISIONS.md`** (D-01…D-11, O-01…O-02). The spec says
   *what*; DECISIONS says *why*, what it cost, and what it forecloses.

These are Trawl's own files — not modified upstream sources — so they carry no GPL §5(a) change
notice. Files inherited from Seal Plus do; see `DECISIONS.md` D-02.
