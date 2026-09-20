# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary user: the video owner. They create a USER account, sign in, send a video, watch its status, inspect processing, and (when the product is later integrated) download extracted images. They are not an administrator. ADMIN exists in the backend and is out of the current interface.

## Product Purpose

FIAP X exists so a person can send a video and follow the extraction of still images without seeing implementation details. Success for this phase is a navigable Portuguese prototype of that journey: cadastro, entrada, lista, detalhe, envio simulado, perfil e saída.

The durable product-target remains asynchronous frame extraction (FFmpeg, ZIP of images). Confirmed for this record: the frontend does not call the API yet; upload, processing, and download are simulated.

## Positioning

The interface speaks only in product language. A neighboring job dashboard could list PENDING jobs and source keys; FIAP X must not. The list shows one owner-facing lifecycle status derived from existing upload and latest-attempt data:

- Pendente
- Processando
- Processado
- Rejeitado
- Expirado
- Falha no processamento

## Operating Context

Six screens: Entrar, Cadastrar, Meus vídeos, Detalhe do vídeo, Enviar vídeo, Meu perfil. Authenticated shell with Meus vídeos, Enviar vídeo, Meu perfil, and Sair. Local demo at `services/video-api-frontend` (`npm run dev`, typically http://localhost:5173). Evaluation is visual and copy review in Portuguese, desktop and mobile, not production telemetry.

## Capabilities and Constraints

Confirmed in the prototype:

- Register USER (email unique, password 8–128 characters); login does not auto-happen after register.
- List videos owned by the signed-in account; one row per file; one lifecycle status.
- Detail shows the same lifecycle status, timeline, and prior processing history.
- Simulated upload: empty picker, selected file, progress, confirmation, success, recoverable failure, pending confirmation. No file bytes leave the browser.
- Profile is read-only email. No profile edit, password recovery, or admin UI.

Constraints:

- No HTTP, JWT, cookies, signed URLs, or real object-storage keys in the UI.
- Do not expose job, sourceKey, resultKey, endpoints, UUIDs, or English internal enums as user-facing copy.
- Frontend stack is already React 19 + TypeScript + Vite in `services/video-api-frontend`.

Undecided: when the prototype is replaced by real API integration, and whether download of the image ZIP ships in the same epic.

## Brand Commitments

Name: FIAP X. Voice: Portuguese (Brazil), second-person product copy, no implementation jargon. Binding status vocabulary listed under Positioning.

Authenticated UI sits alongside Linear, Stripe Dashboard, and Frame.io: a mature product tool, not an editorial poster. Standing preference: compact navy/charcoal chrome, off-white work surface, filename-first list, compact status badges, FIAP accents used sparingly. Not paper-beige, not handwritten, not retro, not scrapbook.

## Evidence on Hand

- Copy inventory: `services/video-api-frontend/docs/prototype-copy.md` and `src/product-copy.ts`
- Demo account (public, not production): `demo@fiapx.local` / `MockPassword123!`
- Backend contracts and architecture live in `contracts/` and `docs/architecture/`; they are not the UI source of truth for this prototype
- No customer testimonials, press, or production screenshots. Do not fabricate them.

## Product Principles

1. The owner only sees their own videos and only the statuses listed above.
2. Language stays in Brazilian Portuguese and never leaks internals.
3. This phase is a reviewable prototype: simulated send/download, no network.
4. Cadastro, sessão e saída existem para o fluxo completo, não como um painel técnico.
5. Future integration must not invent missing API fields in the UI; gaps stay documented.

## Accessibility & Inclusion

Interface copy and status labels are Brazilian Portuguese. No WCAG level was adopted yet. Forms need persistent labels, field errors, and keyboard use; that is existing prototype practice, not a certified standard.
