import { HttpErrorResponse } from '@angular/common/http';

/** Extracts a human-readable message from the backend's {detail: ...} error body. */
export function extractErrorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (err instanceof HttpErrorResponse) {
    const detail = err.error?.detail;
    if (typeof detail === 'string') {
      return detail;
    }
    if (detail && typeof detail === 'object') {
      if ('reason' in detail) {
        return String((detail as any).reason);
      }
      const firstValue = Object.values(detail)[0];
      if (firstValue) {
        return String(firstValue);
      }
    }
  }
  return fallback;
}
