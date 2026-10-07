# HeyHood repository working instructions

Maintain `docs/HEYHOOD_HANDOFF.md` as the living product and engineering handoff. When changing behavior, APIs, data, deployment, or product decisions, update the relevant section and append an entry to its conversation/decision log. Record meaningful validation and distinguish implemented, deployed, and planned behavior. Preserve prior decisions; do not overwrite history with a new narrative.

The handoff is a contextual record, not a guaranteed verbatim chat transcript. Do not claim full transcript coverage unless an actual export is available. Never include API keys, passwords, access tokens, signing private keys, or screenshots containing secrets. Environment variable names are safe to document.

The application lives in `apna-bazaar-git`; the repository root is not the Railway build root. Production deploys from `main`. Android shell source is in `android-shell`. Use the user's current authorization and preferences for deployments and generated artifacts.
