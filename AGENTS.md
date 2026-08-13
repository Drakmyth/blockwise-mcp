# Agent Instructions

## Working style

- Ask one discovery question at a time.
- Show how many planned questions remain.
- Adjust the count explicitly when new questions arise.
- End every turn with one question or a request for verification.
- Challenge assumptions, gaps, and weak reasoning; do not accept answers uncritically.
- Keep responses and project documentation concise.
- Use ASCII punctuation instead of equivalent Unicode punctuation.
- Work on dedicated branches; commit and push focused changes as work progresses.
- Open the pull request directly after completing and validating an implementation phase.
- Keep each pull request buildable, deployable, focused, and ideally at or below 350 changed lines.
- When requesting PR review, provide a squash message with an optional one-line overview followed by a high-value commit log. Preserve meaningful delivered detail; omit or reword redundant entries.
- End squash messages with `Co-authored-by: Codex <codex@openai.com>`.

## Discovery records

- Record findings and decisions in `docs/discovery.md`.
- Distinguish accepted decisions, provisional direction, risks, open questions, and deferred scope.
- Do not begin implementation before requirements and architecture are sufficiently established.
