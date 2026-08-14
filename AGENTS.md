# Agent Instructions

## Working style

- Ask one discovery question at a time, state how many planned questions remain, and adjust the count when new questions arise.
- End every turn with one question or a request for verification.
- Challenge assumptions, gaps, and weak reasoning.
- Keep responses and project documentation concise.
- Use ASCII punctuation and do not use underscores as numeric separators.
- Do not implement until requirements and architecture are sufficiently established and the user approves the plan.
- Add concise Javadocs only when public behavior, defaults, validation, lifecycle, ordering, or failures are not obvious.

## Delivery

- Work on dedicated branches and push focused commits as work progresses.
- Keep pull requests buildable, deployable, focused, and ideally at or below 350 changed lines.
- Open each pull request after completing and validating its implementation phase, and assign it to Drakmyth.
- When requesting review, provide a high-value squash message and copy it to the Windows clipboard with `clip.exe`.
- End squash messages with `Co-authored-by: Codex <codex@openai.com>`.
- After merge, update the default branch, delete the local branch, and prune deleted remote branches.

## Discovery records

- Record accepted decisions, provisional direction, risks, open questions, and deferred scope in `docs/discovery.md`.
- Keep implementation details out of discovery unless they constrain future design.
