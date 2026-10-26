import React, { useState } from 'react';
import { Check, Lock, RefreshCw, Shield } from 'lucide-react';
import {
  LicenceState,
  PRICE_INR,
  canPurchaseAnnual,
  formatInr,
  purchaseAnnual,
  restorePurchases,
} from '../../lib/license';

interface LicenseScreenProps {
  state: LicenceState;
  onChange: (next: LicenceState) => void;
}

export const LicenseScreen: React.FC<LicenseScreenProps> = ({ state, onChange }) => {
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState('');

  const run = async (id: string, fn: () => Promise<LicenceState>) => {
    setBusy(id);
    setError('');
    try {
      onChange(await fn());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Google Play could not complete that purchase.');
    } finally {
      setBusy(null);
    }
  };

  const expired = state.plan === 'expired';

  return (
    <div className="min-h-[100dvh] flex items-center justify-center p-5" style={{ background: 'var(--app)', color: 'var(--text)' }}>
      <div className="w-full max-w-md space-y-6">
        <div className="space-y-2 text-center">
          <div
            className="w-12 h-12 rounded-2xl mx-auto flex items-center justify-center"
            style={{ background: 'color-mix(in srgb, var(--accent) 18%, transparent)', color: 'var(--accent)' }}
          >
            <Lock className="w-5 h-5" />
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {state.sideloadBlocked ? 'This copy cannot be licensed' : expired ? 'Your 30-day trial has ended' : state.subscribed ? 'Yearly plan' : 'One yearly plan'}
          </h1>
          <p className="text-sm leading-relaxed" style={{ color: 'var(--muted)' }}>
            {state.sideloadBlocked
              ? 'This copy was not installed from Google Play. A copied APK cannot carry a subscription — it lives on the Play account that bought it.'
              : `Thirty days free, then ${formatInr(PRICE_INR.annual)} a year. That is the only plan. It renews through Google Play and includes the vault and Gmail sync. Cancel in Play; access lasts until the paid year ends.`}
          </p>
        </div>

        {!state.sideloadBlocked && (
        <div className="space-y-3">
          {state.subscribed ? (
            <div className="p-4 rounded-3xl border space-y-1" style={{ borderColor: 'var(--border)', background: 'var(--card)' }}>
              <div className="text-[10px] font-bold uppercase tracking-[0.16em]" style={{ color: 'var(--good)' }}>Yearly plan</div>
              <p className="text-sm font-semibold">{formatInr(PRICE_INR.annual)} a year is on this Play account. The vault and Gmail stay open until this paid year ends.</p>
            </div>
          ) : (
            <PlanCard
              featured
              eyebrow="Only plan"
              title={`Yearly plan · ${formatInr(PRICE_INR.annual)} / year`}
              body="One subscription. The vault stays open and Gmail sync stays on while the year is paid. There is no lifetime licence and no second product."
              cta={
                !canPurchaseAnnual(state)
                  ? 'Not available on this copy'
                  : busy === 'a' ? 'Opening Play…' : `Subscribe ${formatInr(PRICE_INR.annual)} / year`
              }
              disabled={Boolean(busy) || !canPurchaseAnnual(state)}
              onClick={() => run('a', purchaseAnnual)}
            />
          )}
        </div>
        )}

        <button
          type="button"
          disabled={Boolean(busy)}
          onClick={() => run('restore', restorePurchases)}
          className="w-full py-3 rounded-2xl text-sm font-semibold flex items-center justify-center gap-2 border"
          style={{ borderColor: 'var(--border)', color: 'var(--text)' }}
        >
          <RefreshCw className="w-4 h-4" />
          Restore purchases on this Play account
        </button>

        {error && (
          <p className="text-xs text-center" style={{ color: 'var(--bad)' }}>{error}</p>
        )}

        <p className="text-[11px] leading-relaxed text-center" style={{ color: 'var(--subtle)' }}>
          <Shield className="w-3 h-3 inline mr-1" />
          Payments are processed by Google Play. GrowUrDhan never sees your card. A copied APK cannot carry this licence — it lives on the Play account.
        </p>
      </div>
    </div>
  );
};

const PlanCard: React.FC<{
  eyebrow: string;
  title: string;
  body: string;
  cta: string;
  featured?: boolean;
  disabled: boolean;
  onClick: () => void;
}> = ({ eyebrow, title, body, cta, featured, disabled, onClick }) => (
  <div
    className="p-4 rounded-3xl border space-y-3"
    style={{
      borderColor: featured ? 'var(--accent)' : 'var(--border)',
      background: featured ? 'color-mix(in srgb, var(--accent) 8%, var(--card))' : 'var(--card)',
    }}
  >
    <div className="text-[10px] font-bold uppercase tracking-[0.16em]" style={{ color: featured ? 'var(--accent)' : 'var(--muted)' }}>
      {eyebrow}
    </div>
    <h2 className="text-base font-semibold">{title}</h2>
    <p className="text-xs leading-relaxed" style={{ color: 'var(--muted)' }}>{body}</p>
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="w-full py-3 rounded-2xl text-sm font-bold flex items-center justify-center gap-2"
      style={{ background: 'var(--accent)', color: 'var(--accent-fg)', opacity: disabled ? 0.6 : 1 }}
    >
      <Check className="w-4 h-4" />
      {cta}
    </button>
  </div>
);
