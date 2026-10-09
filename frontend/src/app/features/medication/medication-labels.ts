import { MedicationForm, MedicationUnit } from '../../shared/models/medication';

export const FORM_LABELS: Record<MedicationForm, string> = {
  TABLET: 'Comprimido',
  CAPSULE: 'Cápsula',
  SYRUP: 'Xarope',
  SUSPENSION: 'Suspensão',
  DROPS: 'Gotas',
  CREAM: 'Creme',
  OINTMENT: 'Pomada',
  GEL: 'Gel',
  SPRAY: 'Spray',
  INHALER: 'Inalador',
  INJECTION: 'Injetável',
  POWDER: 'Pó',
  SUPPOSITORY: 'Supositório',
  OTHER: 'Outro',
};

export const UNIT_LABELS: Record<MedicationUnit, string> = {
  UNIT: 'Unidades (comprimidos, cápsulas...)',
  ML: 'Mililitros (ml)',
  G: 'Gramas (g)',
};

export const UNIT_SHORT_LABELS: Record<MedicationUnit, string> = {
  UNIT: 'unidades',
  ML: 'ml',
  G: 'g',
};

export const DEFAULT_UNITS: Record<MedicationForm, MedicationUnit> = {
  TABLET: 'UNIT',
  CAPSULE: 'UNIT',
  SYRUP: 'ML',
  SUSPENSION: 'ML',
  DROPS: 'ML',
  CREAM: 'G',
  OINTMENT: 'G',
  GEL: 'G',
  SPRAY: 'ML',
  INHALER: 'UNIT',
  INJECTION: 'UNIT',
  POWDER: 'G',
  SUPPOSITORY: 'UNIT',
  OTHER: 'UNIT',
};
