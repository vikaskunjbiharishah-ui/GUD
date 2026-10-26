/**
 * GrowUrDhan commercial licence.
 *
 * One Google Play subscription: ₹99 a year. A 30-day trial starts on a
 * genuine Play install (native) or a device-bound clock (web preview /
 * device-test APK). There is no lifetime licence and no separate Gmail price.
 *
 * VITE_GUD_TEST_APK=1 is set only on the GUD-Android-APK branch so a
 * sideloaded debug APK can be tested on a phone. The Play AAB on
 * GUD-Android does not set this flag and still rejects extracted APKs.
 */
export const TRIAL_DAYS = 30;

export const SKU_ANNUAL = 'gud_annual';

export const PRICE_INR = {
  annual: 99,
} as const;

export type LicencePlan = 'trial' | 'annual' | 'expired';

export interface LicenceState {
  plan: LicencePlan;
  trialStartedAt: string | null;
  trialEndsAt: string | null;
  daysLeft: number;
  subscribed: boolean;
  renewsAt: string | null;
  playAccountBound: boolean;
  sideloadBlocked: boolean;
  source: 'play' | 'preview';
}

export const EMPTY_LICENCE: LicenceState = {
  plan: 'trial',
  trialStartedAt: null,
  trialEndsAt: null,
  daysLeft: TRIAL_DAYS,
  subscribed: false,
  renewsAt: null,
  playAccountBound: false,
  sideloadBlocked: false,
  source: 'preview',
};

const LS_TRIAL = 'gud.licence.trialStartedAt';
const LS_ANNUAL_UNTIL = 'gud.licence.annualUntil';

function isoDaysFrom(startIso: string, days: number): string {
  const t = Date.parse(startIso);
  if (!Number.isFinite(t)) return startIso;
  return new Date(t + days * 86400000).toISOString();
}

export function isTestApk(): boolean {
  try {
    return import.meta.env.VITE_GUD_TEST_APK === '1';
  } catch {
    return false;
  }
}

export function daysRemaining(trialStartedAt: string, now = new Date()): number {
  const start = Date.parse(trialStartedAt);
  if (!Number.isFinite(start)) return 0;
  const end = start + TRIAL_DAYS * 86400000;
  return Math.max(0, Math.ceil((end - now.getTime()) / 86400000));
}

export function derivePlan(input: {
  subscribed: boolean;
  trialStartedAt: string | null;
  now?: Date;
}): Pick<LicenceState, 'plan' | 'daysLeft' | 'trialEndsAt'> {
  const now = input.now || new Date();
  if (input.subscribed) {
    return { plan: 'annual', daysLeft: 0, trialEndsAt: null };
  }
  if (input.trialStartedAt) {
    const left = daysRemaining(input.trialStartedAt, now);
    const trialEndsAt = isoDaysFrom(input.trialStartedAt, TRIAL_DAYS);
    if (left > 0) return { plan: 'trial', daysLeft: left, trialEndsAt };
    return { plan: 'expired', daysLeft: 0, trialEndsAt };
  }
  return { plan: 'expired', daysLeft: 0, trialEndsAt: null };
}

export function formatInr(n: number): string {
  return `₹${n.toLocaleString('en-IN')}`;
}

export function isNativeShell(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
}

function invokeErrorMessage(e: unknown): string {
  if (e instanceof Error && e.message) return e.message;
  if (typeof e === 'string' && e) return e;
  if (e && typeof e === 'object') {
    const o = e as Record<string, unknown>;
    if (typeof o.message === 'string' && o.message) return o.message;
    if (typeof o.error === 'string' && o.error) return o.error;
  }
  return '';
}

const APP_FIRST = new Set([
  'cas_health',
  'cas_parse',
  'diag_log',
  'diag_status',
  'diag_set',
  'prices_refresh',
  'gmail_connect',
  'gmail_disconnect',
  'gmail_status',
  'gmail_search',
  'gmail_fetch',
  'gmail_advance',
]);

function redactArgs(args?: Record<string, unknown>): string {
  if (!args) return '';
  const parts: string[] = [];
  for (const [k, v] of Object.entries(args)) {
    if (v == null) continue;
    if (/password|bytesB64|pdfBase64|pan/i.test(k)) {
      parts.push(`${k}Chars=${typeof v === 'string' ? v.length : 0}`);
    } else if (typeof v === 'string') {
      parts.push(`${k}=${v.slice(0, 80)}`);
    } else if (typeof v === 'number' || typeof v === 'boolean') {
      parts.push(`${k}=${String(v)}`);
    } else {
      parts.push(`${k}=${typeof v}`);
    }
  }
  return parts.join(' ');
}

function summarize(value: unknown): string {
  if (value == null) return 'null';
  if (typeof value !== 'object') return String(value).slice(0, 240);
  const o = value as Record<string, unknown>;
  const bits: string[] = [];
  for (const k of ['ok', 'error', 'engine', 'path', 'privatePath', 'writeError', 'code', 'lifetime', 'integrity']) {
    if (o[k] != null) bits.push(`${k}=${String(o[k]).slice(0, 240)}`);
  }
  if (typeof o.payload === 'string') {
    try {
      const parsed = JSON.parse(o.payload) as Record<string, unknown>;
      bits.push(
        `payload.ok=${String(parsed.ok)} code=${String(parsed.code ?? '')} fileType=${String(parsed.fileType ?? '')} error=${String(parsed.error ?? '').slice(0, 240)}`
      );
    } catch {
      bits.push(`payloadChars=${o.payload.length}`);
    }
  }
  return bits.join(' ').slice(0, 700) || 'object';
}

const diagQueue: string[] = [];
let diagPump: Promise<void> | null = null;

function pumpDiag() {
  if (diagPump || !isNativeShell()) return;
  diagPump = (async () => {
    const { invoke } = await import('@tauri-apps/api/core');
    while (diagQueue.length) {
      const next = diagQueue.shift();
      if (!next) continue;
      try {
        await invoke('diag_log', { line: next });
      } catch {
        diagQueue.length = 0;
        break;
      }
    }
  })().finally(() => {
    diagPump = null;
    if (diagQueue.length) pumpDiag();
  });
}

function diagNote(cmd: string, line: string) {
  if (cmd === 'diag_log' || !line) return;
  diagQueue.push(line.slice(0, 1800));
  if (diagQueue.length > 40) diagQueue.splice(0, diagQueue.length - 40);
  pumpDiag();
}

/**
 * Vault and Play stay plugin-first.
 * cas_health, cas_parse, diag_*, and prices_refresh try the app command first
 * (Rust forwards to the Android plugin). Fall back only when that channel is
 * not registered.
 */
export async function nativeCommand<T>(cmd: string, args?: Record<string, unknown>): Promise<T> {
  const { invoke } = await import('@tauri-apps/api/core');
  const hint = redactArgs(args);
  const appFirst = APP_FIRST.has(cmd);
  const channels = appFirst ? [cmd, `plugin:growurdhan|${cmd}`] : [`plugin:growurdhan|${cmd}`, cmd];
  let last: unknown;
  for (let i = 0; i < channels.length; i++) {
    const channel = channels[i];
    try {
      const result = await invoke<T>(channel, args);
      diagNote(cmd, `js ${cmd} via ${channel} ok ${summarize(result)} ${hint}`.trim());
      return result;
    } catch (e) {
      last = e;
      const msg = invokeErrorMessage(e);
      diagNote(cmd, `js ${cmd} via ${channel} FAIL ${msg.slice(0, 500)} ${hint}`.trim());
      // Only switch channels when this channel is missing. A real Python error
      // (FileNotFoundError, bad password, parse failure) must be shown as-is.
      // "Plugin not found" is Tauri's missing-channel error, not a Python error.
      const channelMissing =
        !msg ||
        /unknown command|does not exist|forbidden/i.test(msg) ||
        /not allowed/i.test(msg) ||
        /plugin not found/i.test(msg);
      const tryNext = i === 0 && channelMissing;
      if (!tryNext) throw e instanceof Error ? e : new Error(msg || 'command failed');
    }
  }
  const msg = invokeErrorMessage(last);
  throw last instanceof Error ? last : new Error(msg || 'command failed');
}

function readPreviewState(now = new Date()): LicenceState {
  let trialStartedAt = localStorage.getItem(LS_TRIAL);
  if (!trialStartedAt) {
    trialStartedAt = now.toISOString();
    localStorage.setItem(LS_TRIAL, trialStartedAt);
  }
  const renewsAt = localStorage.getItem(LS_ANNUAL_UNTIL);
  const subscribed = Boolean(renewsAt && Date.parse(renewsAt) > now.getTime());
  const derived = derivePlan({ subscribed, trialStartedAt, now });
  return {
    ...EMPTY_LICENCE,
    ...derived,
    trialStartedAt,
    subscribed,
    renewsAt,
    playAccountBound: subscribed,
    source: 'preview',
  };
}

export async function loadLicence(now = new Date()): Promise<LicenceState> {
  if (isTestApk()) {
    return { ...readPreviewState(now), sideloadBlocked: false, source: 'preview' };
  }
  if (isNativeShell()) {
    try {
      const remote = await nativeCommand<Partial<LicenceState> & { ok?: boolean; integrity?: boolean }>('play_licence_status');
      const subscribed = Boolean(remote.subscribed);
      const trialStartedAt = remote.trialStartedAt || now.toISOString();
      const derived = derivePlan({ subscribed, trialStartedAt, now });
      const sideloadBlocked = remote.integrity === false && !subscribed;
      return {
        ...EMPTY_LICENCE,
        ...derived,
        trialStartedAt,
        subscribed,
        renewsAt: remote.renewsAt || null,
        playAccountBound: subscribed,
        sideloadBlocked,
        source: 'play',
      };
    } catch {
      return { ...EMPTY_LICENCE, plan: 'expired', daysLeft: 0, source: 'play', sideloadBlocked: true };
    }
  }
  return readPreviewState(now);
}

export async function purchaseAnnual(): Promise<LicenceState> {
  const current = await loadLicence();
  if (!canPurchaseAnnual(current)) {
    throw new Error(current.subscribed
      ? 'The ₹99 yearly plan is already on this account.'
      : 'This copy cannot buy the ₹99 yearly plan.');
  }
  if (isTestApk() || !isNativeShell()) {
    const until = new Date();
    until.setFullYear(until.getFullYear() + 1);
    localStorage.setItem(LS_ANNUAL_UNTIL, until.toISOString());
    return loadLicence();
  }
  await nativeCommand('play_purchase', { sku: SKU_ANNUAL, offer: 'subscription' });
  return loadLicence();
}

export async function restorePurchases(): Promise<LicenceState> {
  if (isNativeShell() && !isTestApk()) {
    await nativeCommand('play_restore', {});
  }
  return loadLicence();
}

export function inTrial(state: LicenceState): boolean {
  return state.plan === 'trial' && state.daysLeft > 0;
}

/** The yearly plan includes every feature, including Gmail, while it is paid. */
export function enhancementsEnabled(state: LicenceState): boolean {
  if (inTrial(state)) return true;
  return state.subscribed;
}

export function appUnlocked(state: LicenceState): boolean {
  if (state.sideloadBlocked && !state.subscribed) return false;
  return state.subscribed || state.plan === 'trial';
}

/** The only paid product. Hidden once this Play account already has the year. */
export function canPurchaseAnnual(state: LicenceState): boolean {
  return !state.subscribed && !state.sideloadBlocked;
}

/** Trial includes Gmail. After the trial, Gmail stays on only while ₹99 a year is active. */
export function gmailSyncAllowed(state: LicenceState): boolean {
  if (state.sideloadBlocked && !state.subscribed) return false;
  if (inTrial(state)) return true;
  return state.subscribed;
}
