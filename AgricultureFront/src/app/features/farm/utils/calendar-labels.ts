import { EventTypeAgricole } from '../models/calendar.model';

export const EVENT_TYPE_OPTIONS: { value: EventTypeAgricole; label: string }[] = [
  { value: 'SEMIS', label: 'Sowing' },
  { value: 'IRRIGATION', label: 'Irrigation' },
  { value: 'FERTILISATION', label: 'Fertilization' },
  { value: 'AUTRE', label: 'Other' }
];

export function getEventTypeLabel(code: string): string {
  return EVENT_TYPE_OPTIONS.find((o) => o.value === code)?.label ?? code;
}
